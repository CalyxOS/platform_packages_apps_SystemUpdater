# SPDX-FileCopyrightText: 2018 The Android Open Source Project
# SPDX-License-Identifier: Apache-2.0

# Keep, used in tests.
-keep public class org.calyxos.systemupdater.UpdateManager {
   public int getUpdaterState();
}

# Keep, used in tests.
-keep public class org.calyxos.systemupdater.UpdateConfig {
   public <init>(java.lang.String, java.lang.String, int);
}
