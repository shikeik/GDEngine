package com.goldsprite.solofight;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 纯 JDK 动态代理版 TestRunner
 * 无 Mockito，无 Headless，无 Native 依赖。
 * 专治各种水土不服。
 */
public class GdxTestRunner extends BlockJUnit4ClassRunner {

    private static boolean initialized = false;

    public GdxTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
        initGdxEnvironment();
    }

    private synchronized void initGdxEnvironment() {
        if (initialized) return;

        // 1. 模拟 Application (处理日志)
        Gdx.app = (Application) Proxy.newProxyInstance(
                Application.class.getClassLoader(),
                new Class[]{Application.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        // 拦截日志输出，打印到控制台
                        if (name.equals("log") || name.equals("error") || name.equals("debug")) {
                            String tag = args.length > 0 ? String.valueOf(args[0]) : "";
                            String msg = args.length > 1 ? String.valueOf(args[1]) : "";
                            System.out.println("[" + tag + "] " + msg);
                        }
                        // 拦截 getType，返回 Desktop 防止某些逻辑判断出错
                        if (name.equals("getType")) {
                            return Application.ApplicationType.Desktop;
                        }
                        return defaultValue(method.getReturnType());
                    }
                }
        );

        // 2. 模拟 Graphics (处理时间和帧数)
        Gdx.graphics = (Graphics) Proxy.newProxyInstance(
                Graphics.class.getClassLoader(),
                new Class[]{Graphics.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getDeltaTime")) return 0.016f; // 60FPS
                    if (method.getName().equals("getFrameId")) return 1L;
                    return defaultValue(method.getReturnType());
                }
        );

        // 3. 模拟 GL20 (防空指针，什么都不做)
        Gdx.gl = (GL20) Proxy.newProxyInstance(
                GL20.class.getClassLoader(),
                new Class[]{GL20.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
        Gdx.gl20 = Gdx.gl;

        initialized = true;
    }

    /**
     * 返回基本类型的默认值，防止空指针或类型转换错误
     */
    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == float.class) return 0f;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        return null; // 对象类型返回 null
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
    }
}