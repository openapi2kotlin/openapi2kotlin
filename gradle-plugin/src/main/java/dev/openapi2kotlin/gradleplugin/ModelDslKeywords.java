package dev.openapi2kotlin.gradleplugin;

public abstract class ModelDslKeywords {
    public final OpenApi2KotlinExtension.Serialization Jackson = OpenApi2KotlinExtension.Serialization.Jackson;
    public final OpenApi2KotlinExtension.Serialization KotlinX = OpenApi2KotlinExtension.Serialization.KotlinX;
    public final OpenApi2KotlinExtension.Validation Jakarta = OpenApi2KotlinExtension.Validation.Jakarta;
    public final OpenApi2KotlinExtension.Validation JavaX = OpenApi2KotlinExtension.Validation.JavaX;
    public final OpenApi2KotlinExtension.Validation None = OpenApi2KotlinExtension.Validation.None;
}
