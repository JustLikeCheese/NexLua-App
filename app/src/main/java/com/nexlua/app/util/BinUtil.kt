package com.nexlua.app.util

import com.android.identity.util.UUID

object BinUtil {
    const val LUA_MODULE_TEMPLATE = """
        .class public Lcom/nexlua/%s;
.super Ljava/lang/Object;
.source "You are gay if you are watching me"

# interfaces
.implements Lcom/nexlua/LuaModule;

# direct methods
.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

# virtual methods
.method public load(Lcom/nexlua/LuaContext;)Ljava/nio/Buffer;
    .registers 3
    const-string v0, "%s"
    invoke-static {v0}, Lcom/nexlua/LuaModule$-CC;->toBuffer(Ljava/lang/String;)Ljava/nio/Buffer;
    move-result-object v0
    return-object v0
.end method
        """

    fun binLuaModule(name: String, code: String): String {
        return LUA_MODULE_TEMPLATE.format(name, escapeString(code))
    }

    private fun escapeString(input: String): String {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    fun generateSmaliStringArray(fieldName: String, array: Array<String>): String {
        val sb = StringBuilder()
        if (array.isEmpty()) {
            sb.appendLine("    const/4 v0, 0x0")
            sb.appendLine("    new-array v0, v0, [Ljava/lang/String;")
        } else {
            // 加载所有字符串到 v1, v2, ...
            array.forEachIndexed { index, item ->
                sb.appendLine("    const-string v${index + 1}, \"${escapeString(item)}\"")
            }
            val registers = (1..array.size).joinToString(", ") { "v$it" }
            sb.appendLine("    filled-new-array {$registers}, [Ljava/lang/String;")
            sb.appendLine("    move-result-object v0")
        }
        sb.appendLine("    sput-object v0, Lcom/nexlua/LuaConfig;->$fieldName:[Ljava/lang/String;")
        sb.appendLine()
        return sb.toString()
    }


    fun generateClinitSmali(
        luaEntry: String,
        requiredPermissionsInWelcome: Array<String>,
        requiredPermissions: Array<String>,
        onlyDecompress: Array<String>,
        skipDecompress: Array<String>,
        dexMap: Map<String, String>
    ): String {
        val sb = StringBuilder()
        sb.appendLine(".method static constructor <clinit>()V")
        // 需要的寄存器数量。v0, v1, v2 用于通用操作。然后是数组内容。
        // 为了安全，可以声明多一些寄存器。
        val maxArraySize = listOf(
            requiredPermissionsInWelcome,
            requiredPermissions,
            onlyDecompress,
            skipDecompress
        ).maxOfOrNull { it.size } ?: 0
        sb.appendLine("    .registers ${3 + maxArraySize}")
        sb.appendLine()
        // APP_THEME 和 WELCOME_THEME
        sb.appendLine("    sget v0, Lcom/nexlua/R\$style;->AppTheme:I")
        sb.appendLine("    sput v0, Lcom/nexlua/LuaConfig;->APP_THEME:I")
        sb.appendLine("    sget v0, Lcom/nexlua/R\$style;->AppTheme:I")
        sb.appendLine("    sput v0, Lcom/nexlua/LuaConfig;->WELCOME_THEME:I")
        sb.appendLine()
        // LUA_ENTRY
        sb.appendLine("    const-string v0, \"${escapeString(luaEntry)}\"")
        sb.appendLine("    sput-object v0, Lcom/nexlua/LuaConfig;->LUA_ENTRY:Ljava/lang/String;")
        sb.appendLine()

        // 生成所有字符串数组
        sb.append(
            generateSmaliStringArray(
                "REQUIRED_PERMISSIONS_IN_WELCOME",
                requiredPermissionsInWelcome
            )
        )
        sb.append(generateSmaliStringArray("REQUIRED_PERMISSIONS", requiredPermissions))
        sb.append(generateSmaliStringArray("ONLY_DECOMPRESS", onlyDecompress))
        sb.append(generateSmaliStringArray("SKIP_DECOMPRESS", skipDecompress))

        // LUA_DEX_MAP
        sb.appendLine("    new-instance v0, Ljava/util/HashMap;")
        sb.appendLine("    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V")
        sb.appendLine()
        dexMap.forEach { (fileName, className) ->
            val classPath = "L" + className.replace('.', '/') + ";"
            sb.appendLine("    const-string v1, \"${escapeString(fileName)}\"")
            sb.appendLine("    const-class v2, $classPath")
            sb.appendLine("    invoke-interface {v0, v1, v2}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
            sb.appendLine()
        }
        sb.appendLine("    invoke-static {v0}, Ljava/util/Collections;->unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;")
        sb.appendLine("    move-result-object v0")
        sb.appendLine("    sput-object v0, Lcom/nexlua/LuaConfig;->LUA_DEX_MAP:Ljava/util/Map;")
        sb.appendLine()

        sb.appendLine("    return-void")
        sb.appendLine(".end method")
        return sb.toString()
    }

    const val LUA_CONFIG_TEMPLATE = """
.class public final Lcom/nexlua/LuaConfig;
.super Ljava/lang/Object;
.source "You are gay if you are watching me"

# static fields
.field public static final APP_THEME:I
.field public static final LUA_DEX_MAP:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/Class<",
            "*>;>;"
        }
    .end annotation
.end field
.field public static final LUA_ENTRY:Ljava/lang/String;
.field public static final ONLY_DECOMPRESS:[Ljava/lang/String;
.field public static final REQUIRED_PERMISSIONS:[Ljava/lang/String;
.field public static final REQUIRED_PERMISSIONS_IN_WELCOME:[Ljava/lang/String;
.field public static final SKIP_DECOMPRESS:[Ljava/lang/String;
.field public static final WELCOME_THEME:I

# direct methods
%s

.method public constructor <init>()V
    .registers 1
    .line 9
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method
"""
}