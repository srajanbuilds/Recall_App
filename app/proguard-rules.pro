# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

-dontwarn javax.lang.model.**
-dontwarn javax.annotation.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**

# Keep generic models if needed, though room handles its own rules
-keep class com.recall.app.core.data.model.** { *; }

# ONNX Runtime & JNI rules
-keep class ai.onnxruntime.** { *; }
-keep class com.recall.app.core.ai.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn ai.onnxruntime.**

# Keep Hilt and related classes
-keep class **.Hilt_* { *; }
-keep class com.recall.app.RecallApplication { *; }
