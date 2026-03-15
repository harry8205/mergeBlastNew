package com.mergeblast.data.repository;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.mergeblast.data.models.PlayerData;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlayerDao_Impl implements PlayerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlayerData> __insertionAdapterOfPlayerData;

  private final SharedSQLiteStatement __preparedStmtOfAddCoins;

  private final SharedSQLiteStatement __preparedStmtOfSpendCoins;

  private final SharedSQLiteStatement __preparedStmtOfIncrementMerges;

  private final SharedSQLiteStatement __preparedStmtOfIncrementPowerupsUsed;

  private final SharedSQLiteStatement __preparedStmtOfIncrementAdsWatched;

  private final SharedSQLiteStatement __preparedStmtOfAddExp;

  private final SharedSQLiteStatement __preparedStmtOfAddTrophies;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDailyReward;

  private final SharedSQLiteStatement __preparedStmtOfSetLevel;

  public PlayerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlayerData = new EntityInsertionAdapter<PlayerData>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `player_data` (`id`,`coins`,`totalScore`,`highestBlock`,`gamesPlayed`,`level`,`exp`,`trophies`,`lastDailyReward`,`dailyRewardStreak`,`totalMerges`,`powerupsUsed`,`adsWatched`,`hammersOwned`,`shufflesOwned`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlayerData entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCoins());
        statement.bindLong(3, entity.getTotalScore());
        statement.bindLong(4, entity.getHighestBlock());
        statement.bindLong(5, entity.getGamesPlayed());
        statement.bindLong(6, entity.getLevel());
        statement.bindLong(7, entity.getExp());
        statement.bindLong(8, entity.getTrophies());
        statement.bindLong(9, entity.getLastDailyReward());
        statement.bindLong(10, entity.getDailyRewardStreak());
        statement.bindLong(11, entity.getTotalMerges());
        statement.bindLong(12, entity.getPowerupsUsed());
        statement.bindLong(13, entity.getAdsWatched());
        statement.bindLong(14, entity.getHammersOwned());
        statement.bindLong(15, entity.getShufflesOwned());
      }
    };
    this.__preparedStmtOfAddCoins = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET coins = coins + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfSpendCoins = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET coins = coins - ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementMerges = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET totalMerges = totalMerges + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementPowerupsUsed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET powerupsUsed = powerupsUsed + 1 WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementAdsWatched = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET adsWatched = adsWatched + 1 WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfAddExp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET exp = exp + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfAddTrophies = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET trophies = trophies + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateDailyReward = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET lastDailyReward = ?, dailyRewardStreak = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfSetLevel = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE player_data SET level = ? WHERE id = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final PlayerData playerData,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlayerData.insert(playerData);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addCoins(final int amount, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAddCoins.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, amount);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfAddCoins.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object spendCoins(final int amount, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSpendCoins.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, amount);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSpendCoins.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementMerges(final int count, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementMerges.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, count);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementMerges.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementPowerupsUsed(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementPowerupsUsed.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementPowerupsUsed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementAdsWatched(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementAdsWatched.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementAdsWatched.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object addExp(final int amount, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAddExp.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, amount);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfAddExp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object addTrophies(final int amount, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAddTrophies.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, amount);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfAddTrophies.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDailyReward(final long timestamp, final int streak,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDailyReward.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, streak);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateDailyReward.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setLevel(final int level, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetLevel.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, level);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetLevel.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<PlayerData> getPlayerData() {
    final String _sql = "SELECT * FROM player_data WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"player_data"}, new Callable<PlayerData>() {
      @Override
      @Nullable
      public PlayerData call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCoins = CursorUtil.getColumnIndexOrThrow(_cursor, "coins");
          final int _cursorIndexOfTotalScore = CursorUtil.getColumnIndexOrThrow(_cursor, "totalScore");
          final int _cursorIndexOfHighestBlock = CursorUtil.getColumnIndexOrThrow(_cursor, "highestBlock");
          final int _cursorIndexOfGamesPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "gamesPlayed");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfExp = CursorUtil.getColumnIndexOrThrow(_cursor, "exp");
          final int _cursorIndexOfTrophies = CursorUtil.getColumnIndexOrThrow(_cursor, "trophies");
          final int _cursorIndexOfLastDailyReward = CursorUtil.getColumnIndexOrThrow(_cursor, "lastDailyReward");
          final int _cursorIndexOfDailyRewardStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRewardStreak");
          final int _cursorIndexOfTotalMerges = CursorUtil.getColumnIndexOrThrow(_cursor, "totalMerges");
          final int _cursorIndexOfPowerupsUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "powerupsUsed");
          final int _cursorIndexOfAdsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "adsWatched");
          final int _cursorIndexOfHammersOwned = CursorUtil.getColumnIndexOrThrow(_cursor, "hammersOwned");
          final int _cursorIndexOfShufflesOwned = CursorUtil.getColumnIndexOrThrow(_cursor, "shufflesOwned");
          final PlayerData _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpCoins;
            _tmpCoins = _cursor.getInt(_cursorIndexOfCoins);
            final long _tmpTotalScore;
            _tmpTotalScore = _cursor.getLong(_cursorIndexOfTotalScore);
            final long _tmpHighestBlock;
            _tmpHighestBlock = _cursor.getLong(_cursorIndexOfHighestBlock);
            final int _tmpGamesPlayed;
            _tmpGamesPlayed = _cursor.getInt(_cursorIndexOfGamesPlayed);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpExp;
            _tmpExp = _cursor.getInt(_cursorIndexOfExp);
            final int _tmpTrophies;
            _tmpTrophies = _cursor.getInt(_cursorIndexOfTrophies);
            final long _tmpLastDailyReward;
            _tmpLastDailyReward = _cursor.getLong(_cursorIndexOfLastDailyReward);
            final int _tmpDailyRewardStreak;
            _tmpDailyRewardStreak = _cursor.getInt(_cursorIndexOfDailyRewardStreak);
            final int _tmpTotalMerges;
            _tmpTotalMerges = _cursor.getInt(_cursorIndexOfTotalMerges);
            final int _tmpPowerupsUsed;
            _tmpPowerupsUsed = _cursor.getInt(_cursorIndexOfPowerupsUsed);
            final int _tmpAdsWatched;
            _tmpAdsWatched = _cursor.getInt(_cursorIndexOfAdsWatched);
            final int _tmpHammersOwned;
            _tmpHammersOwned = _cursor.getInt(_cursorIndexOfHammersOwned);
            final int _tmpShufflesOwned;
            _tmpShufflesOwned = _cursor.getInt(_cursorIndexOfShufflesOwned);
            _result = new PlayerData(_tmpId,_tmpCoins,_tmpTotalScore,_tmpHighestBlock,_tmpGamesPlayed,_tmpLevel,_tmpExp,_tmpTrophies,_tmpLastDailyReward,_tmpDailyRewardStreak,_tmpTotalMerges,_tmpPowerupsUsed,_tmpAdsWatched,_tmpHammersOwned,_tmpShufflesOwned);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPlayerDataOnce(final Continuation<? super PlayerData> $completion) {
    final String _sql = "SELECT * FROM player_data WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlayerData>() {
      @Override
      @Nullable
      public PlayerData call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCoins = CursorUtil.getColumnIndexOrThrow(_cursor, "coins");
          final int _cursorIndexOfTotalScore = CursorUtil.getColumnIndexOrThrow(_cursor, "totalScore");
          final int _cursorIndexOfHighestBlock = CursorUtil.getColumnIndexOrThrow(_cursor, "highestBlock");
          final int _cursorIndexOfGamesPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "gamesPlayed");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfExp = CursorUtil.getColumnIndexOrThrow(_cursor, "exp");
          final int _cursorIndexOfTrophies = CursorUtil.getColumnIndexOrThrow(_cursor, "trophies");
          final int _cursorIndexOfLastDailyReward = CursorUtil.getColumnIndexOrThrow(_cursor, "lastDailyReward");
          final int _cursorIndexOfDailyRewardStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyRewardStreak");
          final int _cursorIndexOfTotalMerges = CursorUtil.getColumnIndexOrThrow(_cursor, "totalMerges");
          final int _cursorIndexOfPowerupsUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "powerupsUsed");
          final int _cursorIndexOfAdsWatched = CursorUtil.getColumnIndexOrThrow(_cursor, "adsWatched");
          final int _cursorIndexOfHammersOwned = CursorUtil.getColumnIndexOrThrow(_cursor, "hammersOwned");
          final int _cursorIndexOfShufflesOwned = CursorUtil.getColumnIndexOrThrow(_cursor, "shufflesOwned");
          final PlayerData _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpCoins;
            _tmpCoins = _cursor.getInt(_cursorIndexOfCoins);
            final long _tmpTotalScore;
            _tmpTotalScore = _cursor.getLong(_cursorIndexOfTotalScore);
            final long _tmpHighestBlock;
            _tmpHighestBlock = _cursor.getLong(_cursorIndexOfHighestBlock);
            final int _tmpGamesPlayed;
            _tmpGamesPlayed = _cursor.getInt(_cursorIndexOfGamesPlayed);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpExp;
            _tmpExp = _cursor.getInt(_cursorIndexOfExp);
            final int _tmpTrophies;
            _tmpTrophies = _cursor.getInt(_cursorIndexOfTrophies);
            final long _tmpLastDailyReward;
            _tmpLastDailyReward = _cursor.getLong(_cursorIndexOfLastDailyReward);
            final int _tmpDailyRewardStreak;
            _tmpDailyRewardStreak = _cursor.getInt(_cursorIndexOfDailyRewardStreak);
            final int _tmpTotalMerges;
            _tmpTotalMerges = _cursor.getInt(_cursorIndexOfTotalMerges);
            final int _tmpPowerupsUsed;
            _tmpPowerupsUsed = _cursor.getInt(_cursorIndexOfPowerupsUsed);
            final int _tmpAdsWatched;
            _tmpAdsWatched = _cursor.getInt(_cursorIndexOfAdsWatched);
            final int _tmpHammersOwned;
            _tmpHammersOwned = _cursor.getInt(_cursorIndexOfHammersOwned);
            final int _tmpShufflesOwned;
            _tmpShufflesOwned = _cursor.getInt(_cursorIndexOfShufflesOwned);
            _result = new PlayerData(_tmpId,_tmpCoins,_tmpTotalScore,_tmpHighestBlock,_tmpGamesPlayed,_tmpLevel,_tmpExp,_tmpTrophies,_tmpLastDailyReward,_tmpDailyRewardStreak,_tmpTotalMerges,_tmpPowerupsUsed,_tmpAdsWatched,_tmpHammersOwned,_tmpShufflesOwned);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
