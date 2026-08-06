# Elimina las llamadas a Log en release (R8).
# Ninguna seed, secreto, entropia o clave derivada debe poder salir por logcat.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Compose/Material3 ya traen sus propias reglas de consumer rules.
