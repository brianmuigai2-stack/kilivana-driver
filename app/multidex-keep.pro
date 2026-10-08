# Force the Application class and the network entry point it
# touches during init into the primary dex. On low-RAM devices
# the secondary dexes are not always extracted before the
# Application class is instantiated, which crashes startup with
# a ClassNotFoundException for KilivanaApp.
-keep class com.example.kilivana_driver.KilivanaApp { *; }
-keep class com.example.kilivana_driver.data.network.ApiClient { *; }
-keep class com.example.kilivana_driver.data.network.ApiClient$* { *; }

# Keep the debug build debuggable: we only want R8 to compute the
# main-dex list, not to shrink, rename or optimize anything.
-dontshrink
-dontobfuscate
-dontoptimize
