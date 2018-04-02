/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.livedata;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.util.DatabaseUtil;


public class KeyInfoInteractor {
    // These are the rows that we will retrieve.
    private String[] QUERY_PROJECTION = new String[]{
            KeyRings._ID,
            KeyRings.MASTER_KEY_ID,
            KeyRings.CREATION,
            KeyRings.HAS_ENCRYPT,
            KeyRings.HAS_AUTHENTICATE_SECRET,
            KeyRings.HAS_ANY_SECRET,
            KeyRings.VERIFIED,
            KeyRings.NAME,
            KeyRings.EMAIL,
            KeyRings.COMMENT,
            KeyRings.IS_EXPIRED,
            KeyRings.IS_REVOKED,
    };
    private static final int INDEX_MASTER_KEY_ID = 1;
    private static final int INDEX_CREATION = 2;
    private static final int INDEX_HAS_ENCRYPT = 3;
    private static final int INDEX_HAS_AUTHENTICATE = 4;
    private static final int INDEX_HAS_ANY_SECRET = 5;
    private static final int INDEX_VERIFIED = 6;
    private static final int INDEX_NAME = 7;
    private static final int INDEX_EMAIL = 8;
    private static final int INDEX_COMMENT = 9;

    private static final String QUERY_WHERE_ALL = Tables.KEYS + "." + KeyRings.IS_REVOKED +
            " = 0 AND " + KeyRings.IS_EXPIRED + " = 0";
    private static final String QUERY_WHERE_SECRET = Tables.KEYS + "." + KeyRings.IS_REVOKED +
            " = 0 AND " + KeyRings.IS_EXPIRED + " = 0" + " AND " + KeyRings.HAS_ANY_SECRET + " != 0";
    private static final String QUERY_ORDER = Tables.KEYS + "." + KeyRings.CREATION + " DESC";

    private final ContentResolver contentResolver;


    public KeyInfoInteractor(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public List<KeyInfo> loadKeyInfo(KeySelector keySelector) {
        ArrayList<KeyInfo> keyInfos = new ArrayList<>();
        Cursor cursor;

        String selection = keySelector.isOnlySecret() ? QUERY_WHERE_SECRET : QUERY_WHERE_ALL;
        String additionalSelection = keySelector.getSelection();

        selection = DatabaseUtil.concatenateWhere(selection, additionalSelection);
        cursor = contentResolver.query(keySelector.getKeyRingUri(), QUERY_PROJECTION, selection, null, QUERY_ORDER);

        if (cursor == null) {
            return null;
        }

        while (cursor.moveToNext()) {
            KeyInfo keyInfo = KeyInfo.fromCursor(cursor);
            keyInfos.add(keyInfo);
        }

        return Collections.unmodifiableList(keyInfos);
    }

    @AutoValue
    public abstract static class KeyInfo {
        public abstract long getMasterKeyId();
        public abstract long getCreationDate();
        public abstract boolean getHasEncrypt();
        public abstract boolean getHasAuthenticate();
        public abstract boolean getHasAnySecret();
        public abstract boolean getIsVerified();

        @Nullable
        public abstract String getName();
        @Nullable
        public abstract String getEmail();
        @Nullable
        public abstract String getComment();

        static KeyInfo fromCursor(Cursor cursor) {
            long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
            long creationDate = cursor.getLong(INDEX_CREATION) * 1000L;
            boolean hasEncrypt = cursor.getInt(INDEX_HAS_ENCRYPT) != 0;
            boolean hasAuthenticate = cursor.getInt(INDEX_HAS_AUTHENTICATE) != 0;
            boolean hasAnySecret = cursor.getInt(INDEX_HAS_ANY_SECRET) != 0;
            boolean isVerified = cursor.getInt(INDEX_VERIFIED) == 2;

            String name = cursor.getString(INDEX_NAME);
            String email = cursor.getString(INDEX_EMAIL);
            String comment = cursor.getString(INDEX_COMMENT);

            return new AutoValue_KeyInfoInteractor_KeyInfo(
                    masterKeyId, creationDate, hasEncrypt, hasAuthenticate, hasAnySecret, isVerified, name, email, comment);
        }
    }

    @AutoValue
    public abstract static class KeySelector {
        public abstract Uri getKeyRingUri();
        @Nullable
        public abstract String getSelection();
        public abstract boolean isOnlySecret();

        public static KeySelector create(Uri keyRingUri, String selection) {
            return new AutoValue_KeyInfoInteractor_KeySelector(keyRingUri, selection, false);
        }

        public static KeySelector createOnlySecret(Uri keyRingUri, String selection) {
            return new AutoValue_KeyInfoInteractor_KeySelector(keyRingUri, selection, true);
        }
    }
}
