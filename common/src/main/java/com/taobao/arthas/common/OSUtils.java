package com.taobao.arthas.common;

import java.io.File;
import java.util.Locale;

/**
 * 操作系统工具类
 * 提供获取和判断当前操作系统类型、CPU架构等信息的工具方法
 * 用于识别运行环境，以便执行平台相关的操作
 *
 * @author hengyunabc 2018-11-08
 *
 */
public class OSUtils {
    /**
     * 操作系统名称
     * 从系统属性"os.name"中获取，转换为小写字母
     * 例如：windows、linux、mac os x等
     */
    private static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    /**
     * 操作系统架构
     * 从系统属性"os.arch"中获取，转换为小写字母
     * 例如：x86_64、aarch64等
     */
    private static final String OPERATING_SYSTEM_ARCH = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    /**
     * 未知平台标识
     */
    private static final String UNKNOWN = "unknown";

    /**
     * 当前操作系统平台
     * 在静态初始化块中根据系统属性自动识别
     */
    static PlatformEnum platform;

    /**
     * 当前CPU架构
     * 在静态初始化块中规范化后的架构名称
     * 例如：x86_64、aarch_64、arm_32等
     */
    static String arch;

    /**
     * 静态初始化块
     * 在类加载时执行，用于识别操作系统平台和CPU架构
     */
    static {
        // 判断操作系统类型
        if (OPERATING_SYSTEM_NAME.startsWith("linux")) {
            // Linux操作系统
            platform = PlatformEnum.LINUX;
        } else if (OPERATING_SYSTEM_NAME.startsWith("mac") || OPERATING_SYSTEM_NAME.startsWith("darwin")) {
            // macOS操作系统
            platform = PlatformEnum.MACOSX;
        } else if (OPERATING_SYSTEM_NAME.startsWith("windows")) {
            // Windows操作系统
            platform = PlatformEnum.WINDOWS;
        } else {
            // 未知操作系统
            platform = PlatformEnum.UNKNOWN;
        }

        // 规范化CPU架构名称
        arch = normalizeArch(OPERATING_SYSTEM_ARCH);
    }

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的，不需要创建实例
     */
    private OSUtils() {
    }

    /**
     * 判断当前是否运行在Windows操作系统上
     *
     * @return 如果是Windows系统返回true，否则返回false
     */
    public static boolean isWindows() {
        return platform == PlatformEnum.WINDOWS;
    }

    /**
     * 判断当前是否运行在Linux操作系统上
     *
     * @return 如果是Linux系统返回true，否则返回false
     */
    public static boolean isLinux() {
        return platform == PlatformEnum.LINUX;
    }

    /**
     * 判断当前是否运行在macOS操作系统上
     *
     * @return 如果是macOS系统返回true，否则返回false
     */
    public static boolean isMac() {
        return platform == PlatformEnum.MACOSX;
    }

    /**
     * 判断当前是否运行在Cygwin或MinGW环境下
     * 这些是Windows上的Unix兼容层
     *
     * @return 如果是Cygwin或MinGW环境返回true，否则返回false
     */
    public static boolean isCygwinOrMinGW() {
        // 首先检查是否为Windows系统
        if (isWindows()) {
            // 检查MSYSTEM环境变量（MinGW环境变量）
            if ((System.getenv("MSYSTEM") != null && System.getenv("MSYSTEM").startsWith("MINGW"))
                            || "/bin/bash".equals(System.getenv("SHELL"))) {
                // MSYSTEM环境变量存在且以MINGW开头，或者SHELL为/bin/bash
                return true;
            }
        }
        return false;
    }

	/**
	 * 获取当前CPU架构
	 *
	 * @return CPU架构字符串，如"x86_64"、"aarch_64"等
	 */
	public static String arch() {
		return arch;
	}

	/**
	 * 判断当前CPU架构是否为32位ARM
	 *
	 * @return 如果是32位ARM架构返回true，否则返回false
	 */
	public static boolean isArm32() {
		return "arm_32".equals(arch);
	}

	/**
	 * 判断当前CPU架构是否为64位ARM
	 *
	 * @return 如果是64位ARM架构返回true，否则返回false
	 */
	public static boolean isArm64() {
		return "aarch_64".equals(arch);
	}

	/**
	 * 判断当前CPU架构是否为32位x86
	 *
	 * @return 如果是32位x86架构返回true，否则返回false
	 */
	public static boolean isX86() {
    	return "x86_32".equals(arch);
	}

	/**
	 * 判断当前CPU架构是否为64位x86
	 *
	 * @return 如果是64位x86架构返回true，否则返回false
	 */
	public static boolean isX86_64() {
		return "x86_64".equals(arch);
	}

	/**
	 * 判断当前CPU架构是否为64位LoongArch（龙芯）
	 * LoongArch是中国自主研发的指令集架构
	 *
	 * @return 如果是64位LoongArch架构返回true，否则返回false
	 */
       public static boolean isLoongArch64() {
               return "loongarch_64".equals(arch);
       }


	/**
	 * 规范化CPU架构名称
	 * 将各种不同的架构名称转换为统一的标准格式
	 * 例如：amd64、x64 -> x86_64，aarch64 -> aarch_64
	 *
	 * @param value 原始架构名称
	 * @return 规范化后的架构名称
	 */
	private static String normalizeArch(String value) {
		// 先进行基本的规范化处理（转小写、移除特殊字符）
		value = normalize(value);
		 // 匹配64位x86架构的各种别名
		if (value.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
			return "x86_64";
		}
		// 匹配32位x86架构的各种别名
		if (value.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
			return "x86_32";
		}
		// 匹配64位Itanium架构
		if (value.matches("^(ia64w?|itanium64)$")) {
			return "itanium_64";
		}
		// 匹配32位Itanium架构
		if ("ia64n".equals(value)) {
			return "itanium_32";
		}
		// 匹配32位SPARC架构
		if (value.matches("^(sparc|sparc32)$")) {
			return "sparc_32";
		}
		// 匹配64位SPARC架构
		if (value.matches("^(sparcv9|sparc64)$")) {
			return "sparc_64";
		}
		// 匹配32位ARM架构
		if (value.matches("^(arm|arm32)$")) {
			return "arm_32";
		}
		// 匹配64位ARM架构
		if ("aarch64".equals(value)) {
			return "aarch_64";
		}
		// 匹配32位MIPS架构
		if (value.matches("^(mips|mips32)$")) {
			return "mips_32";
		}
		// 匹配32位小端序MIPS架构
		if (value.matches("^(mipsel|mips32el)$")) {
			return "mipsel_32";
		}
		// 匹配64位MIPS架构
		if ("mips64".equals(value)) {
			return "mips_64";
		}
		// 匹配64位小端序MIPS架构
		if ("mips64el".equals(value)) {
			return "mipsel_64";
		}
		// 匹配32位PowerPC架构
		if (value.matches("^(ppc|ppc32)$")) {
			return "ppc_32";
		}
		// 匹配32位小端序PowerPC架构
		if (value.matches("^(ppcle|ppc32le)$")) {
			return "ppcle_32";
		}
		// 匹配64位PowerPC架构
		if ("ppc64".equals(value)) {
			return "ppc_64";
		}
		// 匹配64位小端序PowerPC架构
		if ("ppc64le".equals(value)) {
			return "ppcle_64";
		}
		// 匹配32位IBM System z架构
		if ("s390".equals(value)) {
			return "s390_32";
		}
		// 匹配64位IBM System z架构
		if ("s390x".equals(value)) {
			return "s390_64";
		}
		// 未知架构，返回原值
		return value;
	}

	/**
	 * 判断当前系统是否使用musl libc库
	 * musl是一个轻量级的C标准库，主要用于嵌入式Linux系统
	 * 通过检查特定的musl库文件是否存在来判断
	 *
	 * @return 如果使用musl libc返回true，否则返回false
	 */
	public static boolean isMuslLibc() {
		// x86_64架构的musl库文件
		File ld_musl_x86_64_file = new File("/lib/ld-musl-x86_64.so.1");
		// aarch64架构的musl库文件
		File ld_musl_aarch64_file = new File("/lib/ld-musl-aarch64.so.1");

		// 检查任一架构的musl库文件是否存在
		if(ld_musl_x86_64_file.exists() || ld_musl_aarch64_file.exists()){
			return true;
		}

		return false;
	}

	/**
	 * 规范化字符串
	 * 将字符串转换为小写，并移除所有非字母数字字符
	 * 用于统一处理不同格式的架构名称
	 *
	 * @param value 原始字符串
	 * @return 规范化后的字符串
	 */
	private static String normalize(String value) {
		// 如果值为null，返回空字符串
		if (value == null) {
			return "";
		}
		// 转换为小写（使用US Locale）并移除所有非字母数字字符
		return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
	}
}
