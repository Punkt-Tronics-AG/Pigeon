package org.thoughtcrime.securesms.recipients;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.signal.core.util.ThreadUtil;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.concurrent.TracingExecutorService;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.RecipientDatabase;
import org.thoughtcrime.securesms.database.RecipientDatabase.MissingRecipientException;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.util.CursorUtil;
import org.thoughtcrime.securesms.util.LRUCache;
import org.thoughtcrime.securesms.util.Stopwatch;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.concurrent.FilteredExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LiveRecipientCache {

  private static final String TAG = Log.tag(LiveRecipientCache.class);

  private static final int CACHE_MAX              = 1000;
  private static final int THREAD_CACHE_WARM_MAX  = 500;
  private static final int CONTACT_CACHE_WARM_MAX = 50;

  private final Context                         context;
  private final RecipientDatabase               recipientDatabase;
  private final Map<RecipientId, LiveRecipient> recipients;
  private final LiveRecipient                   unknown;
  private final Executor                        resolveExecutor;
  private final SQLiteDatabase                  db;

  private final AtomicReference<RecipientId> localRecipientId;
  private final AtomicBoolean                warmedUp;

  @SuppressLint("UseSparseArrays")
  public LiveRecipientCache(@NonNull Context context) {
    this.context           = context.getApplicationContext();
    this.recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
    this.recipients        = new LRUCache<>(CACHE_MAX);
    this.warmedUp          = new AtomicBoolean(false);
    this.localRecipientId  = new AtomicReference<>(null);
    this.unknown           = new LiveRecipient(context, new MutableLiveData<>(), Recipient.UNKNOWN);
    this.db                = DatabaseFactory.getInstance(context).getRawDatabase();
    this.resolveExecutor   = ThreadUtil.trace(new FilteredExecutor(SignalExecutors.BOUNDED, () -> !db.inTransaction()));
  }

  @AnyThread
  @NonNull LiveRecipient getLive(@NonNull RecipientId id) {
    if (id.isUnknown()) return unknown;

    LiveRecipient live;
    boolean       needsResolve;

    synchronized (recipients) {
      live = recipients.get(id);

      if (live == null) {
        live = new LiveRecipient(context, new MutableLiveData<>(), new Recipient(id));
        recipients.put(id, live);
        needsResolve = true;
      } else {
        needsResolve = false;
      }
    }

    if (needsResolve) {
      resolveExecutor.execute(live::resolve);
    }

    return live;
  }

  /**
   * Handles remapping cache entries when recipients are merged.
   */
  public void remap(@NonNull RecipientId oldId, @NonNull RecipientId newId) {
    synchronized (recipients) {
      if (recipients.containsKey(newId)) {
        recipients.put(oldId, recipients.get(newId));
      } else {
        recipients.remove(oldId);
      }
    }
  }

  /**
   * Adds a recipient to the cache if we don't have an entry. This will also update a cache entry
   * if the provided recipient is resolved, or if the existing cache entry is unresolved.
   *
   * If the recipient you add is unresolved, this will enqueue a resolve on a background thread.
   */
  @AnyThread
  public void addToCache(@NonNull Collection<Recipient> newRecipients) {
    newRecipients.stream().filter(this::isValidForCache).forEach(recipient -> {
      LiveRecipient live;
      boolean       needsResolve;

      synchronized (recipients) {
        live = recipients.get(recipient.getId());

        if (live == null) {
          live = new LiveRecipient(context,new MutableLiveData<>(), recipient);
          recipients.put(recipient.getId(), live);
          needsResolve = recipient.isResolving();
        } else if (live.get().isResolving() || !recipient.isResolving()) {
          live.set(recipient);
          needsResolve = recipient.isResolving();
        } else {
          needsResolve = false;
        }
      }

      if (needsResolve) {
        LiveRecipient toResolve = live;

        MissingRecipientException prettyStackTraceError = new MissingRecipientException(toResolve.getId());
        resolveExecutor.execute(() -> {
          try {
            toResolve.resolve();
          } catch (MissingRecipientException e) {
            throw prettyStackTraceError;
          }
        });
      }
    });
  }

  @NonNull Recipient getSelf() {
    RecipientId selfId;

    synchronized (localRecipientId) {
      selfId = localRecipientId.get();
    }

    if (selfId == null) {
      UUID localUuid = TextSecurePreferences.getLocalUuid(context);
      String localE164 = TextSecurePreferences.getLocalNumber(context);

      if (localUuid != null) {
        selfId = recipientDatabase.getByUuid(localUuid).or(recipientDatabase.getByE164(localE164)).orNull();
      } else if (localE164 != null) {
        selfId = recipientDatabase.getByE164(localE164).orNull();
      } else {
        throw new IllegalStateException("Tried to call getSelf() before local data was set!");
      }

      if (selfId == null) {
        throw new MissingRecipientException(null);
      }

      synchronized (localRecipientId) {
        if (localRecipientId.get() == null) {
          localRecipientId.set(selfId);
        }
      }
    }

    return getLive(selfId).resolve();
  }

  @AnyThread
  public void warmUp() {
    if (warmedUp.getAndSet(true)) {
      return;
    }

    Stopwatch stopwatch = new Stopwatch("recipient-warm-up");

    SignalExecutors.BOUNDED.execute(() -> {
      ThreadDatabase  threadDatabase = DatabaseFactory.getThreadDatabase(context);
      List<Recipient> recipients     = new ArrayList<>();

      try (ThreadDatabase.Reader reader = threadDatabase.readerFor(threadDatabase.getRecentConversationList(THREAD_CACHE_WARM_MAX, false, false))) {
        int          i      = 0;
        ThreadRecord record = null;

        while ((record = reader.getNext()) != null && i < THREAD_CACHE_WARM_MAX) {
          recipients.add(record.getRecipient());
          i++;
        }
      }

      Log.d(TAG, "Warming up " + recipients.size() + " thread recipients.");
      addToCache(recipients);

      stopwatch.split("thread");

      if (SignalStore.registrationValues().isRegistrationComplete()) {
        try (Cursor cursor = DatabaseFactory.getRecipientDatabase(context).getNonGroupContacts(false)) {
          int count = 0;
          while (cursor != null && cursor.moveToNext() && count < CONTACT_CACHE_WARM_MAX) {
            RecipientId id = RecipientId.from(CursorUtil.requireLong(cursor, RecipientDatabase.ID));
            Recipient.resolved(id);
            count++;
          }

          Log.d(TAG, "Warmed up " + count + " contact recipient.");

          stopwatch.split("contact");
        }
      }

      stopwatch.stop(TAG);
    });
  }

  @AnyThread
  public void clearSelf() {
    synchronized (localRecipientId) {
      localRecipientId.set(null);
    }
  }

  @AnyThread
  public void clear() {
    synchronized (recipients) {
      recipients.clear();
    }
  }

  private boolean isValidForCache(@NonNull Recipient recipient) {
    return !recipient.getId().isUnknown() && (recipient.hasServiceIdentifier() || recipient.getGroupId().isPresent() || recipient.hasSmsAddress());
  }
}
