package com.taobao.arthas.core.util.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * 组合匹配器接口
 * <p>
 * 该接口扩展了{@link Matcher}接口，提供了组合多个匹配器的能力。
 * 组合匹配器允许将多个匹配器按照特定的逻辑关系（与、或）组合起来，
 * 形成更复杂的匹配规则。
 * </p>
 * <p>
 * 该接口提供了两种内置的组合方式：
 * <ul>
 *   <li>{@link And}：与关系组合，所有子匹配器都匹配时才匹配</li>
 *   <li>{@link Or}：或关系组合，任一子匹配器匹配时就匹配</li>
 * </ul>
 * </p>
 *
 * @param <T> 待匹配的目标对象类型
 * @author ralf0131
 * @since 2017-01-06
 */
public interface GroupMatcher<T> extends Matcher<T> {

    /**
     * 向组合匹配器中添加一个新的子匹配器
     * <p>
     * 该方法用于动态扩展组合匹配器的子匹配器列表，
     * 可以在运行时添加新的匹配条件。
     * </p>
     *
     * @param matcher 要添加的子匹配器，不能为null
     */
    void add(Matcher<T> matcher);

    /**
     * 与关系组合匹配器
     * <p>
     * 该类实现了"逻辑与"的组合匹配规则。
     * 只有当目标对象满足所有子匹配器的匹配条件时，
     * 才认为匹配成功。
     * </p>
     * <p>
     * 该匹配器采用短路策略：一旦发现某个子匹配器不匹配，
     * 立即返回false，不再继续检查剩余的匹配器。
     * </p>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>需要同时满足多个条件的匹配</li>
     *   <li>例如：类名以"com.taobao"开头且包含"Service"的类</li>
     * </ul>
     * </p>
     *
     * @param <T> 待匹配的目标对象类型
     */
    class And<T> implements GroupMatcher<T> {

        /**
         * 子匹配器集合
         * <p>
         * 该集合存储了所有需要进行"与"组合的子匹配器。
         * 目标对象必须匹配该集合中的所有匹配器。
         * </p>
         */
        private final Collection<Matcher<T>> matchers;

        /**
         * 构造与关系组合匹配器
         * <p>
         * 使用可变参数方式接收多个子匹配器，
         * 目标对象必须匹配所有这些子匹配器才认为匹配成功。
         * </p>
         * <p>
         * 短路行为：
         * 当遇到第一个不匹配的子匹配器时，立即返回false。
         * </p>
         *
         * @param matchers 可变数量的子匹配器数组，
         *                 目标对象必须匹配数组中的所有匹配器
         */
        public And(Matcher<T>... matchers) {
            this.matchers = Arrays.asList(matchers);
        }

        /**
         * 判断目标对象是否匹配所有子匹配器
         * <p>
         * 该方法遍历所有子匹配器，检查目标对象是否匹配每一个匹配器。
         * 采用短路策略：一旦发现某个子匹配器返回false，
         * 立即返回false，不再继续检查剩余的匹配器。
         * </p>
         *
         * @param target 待匹配的目标对象
         * @return 如果目标对象匹配所有子匹配器则返回true，
         *         否则返回false（空匹配器集合返回true）
         */
        @Override
        public boolean matching(T target) {
            // 遍历所有子匹配器
            for (Matcher<T> matcher : matchers) {
                // 如果任一子匹配器不匹配，立即返回false
                if (!matcher.matching(target)) {
                    return false;
                }
            }
            // 所有子匹配器都匹配，返回true
            return true;
        }

        /**
         * 向组合匹配器中添加一个新的子匹配器
         * <p>
         * 该方法用于动态扩展"与"组合的子匹配器列表。
         * 添加新的匹配器后，目标对象需要同时满足原有匹配器和
         * 新添加的匹配器才能匹配成功。
         * </p>
         *
         * @param matcher 要添加的子匹配器，不能为null
         */
        @Override
        public void add(Matcher<T> matcher) {
            matchers.add(matcher);
        }
    }

    /**
     * 或关系组合匹配器
     * <p>
     * 该类实现了"逻辑或"的组合匹配规则。
     * 只要目标对象满足任意一个子匹配器的匹配条件，
     * 就认为匹配成功。
     * </p>
     * <p>
     * 该匹配器采用短路策略：一旦发现某个子匹配器匹配，
     * 立即返回true，不再继续检查剩余的匹配器。
     * </p>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>需要满足多个条件中任意一个的匹配</li>
     *   <li>例如：匹配名为"toString"或"hashCode"的方法</li>
     * </ul>
     * </p>
     *
     * @param <T> 待匹配的目标对象类型
     */
    class Or<T> implements GroupMatcher<T> {

        /**
         * 子匹配器集合
         * <p>
         * 该集合存储了所有需要进行"或"组合的子匹配器。
         * 目标对象只需要匹配该集合中的任意一个匹配器即可。
         * </p>
         */
        private final Collection<Matcher<T>> matchers;

        /**
         * 构造空的或关系组合匹配器
         * <p>
         * 创建一个不包含任何子匹配器的空组合匹配器。
         * 可以通过{@link #add(Matcher)}方法后续添加子匹配器。
         * </p>
         * <p>
         * 注意：空匹配器集合的matching方法总是返回false。
         * </p>
         */
        public Or() {
            this.matchers = new ArrayList<Matcher<T>>();
        }

        /**
         * 构造或关系组合匹配器
         * <p>
         * 使用可变参数方式接收多个子匹配器，
         * 目标对象只需要匹配这些子匹配器中的任意一个就认为匹配成功。
         * </p>
         * <p>
         * 短路行为：
         * 当遇到第一个匹配的子匹配器时，立即返回true。
         * </p>
         *
         * @param matchers 可变数量的子匹配器数组，
         *                 目标对象只需要匹配数组中的任意一个匹配器
         */
        public Or(Matcher<T>... matchers) {
            this.matchers = Arrays.asList(matchers);
        }

        /**
         * 构造或关系组合匹配器
         * <p>
         * 使用集合方式接收多个子匹配器，
         * 目标对象只需要匹配这些子匹配器中的任意一个就认为匹配成功。
         * </p>
         *
         * @param matchers 子匹配器集合，
         *                 目标对象只需要匹配集合中的任意一个匹配器
         */
        public Or(Collection<Matcher<T>> matchers) {
            this.matchers = matchers;
        }

        /**
         * 判断目标对象是否匹配任意一个子匹配器
         * <p>
         * 该方法遍历所有子匹配器，检查目标对象是否匹配其中任意一个。
         * 采用短路策略：一旦发现某个子匹配器返回true，
         * 立即返回true，不再继续检查剩余的匹配器。
         * </p>
         *
         * @param target 待匹配的目标对象
         * @return 如果目标对象匹配任意一个子匹配器则返回true，
         *         否则返回false（空匹配器集合返回false）
         */
        @Override
        public boolean matching(T target) {
            // 遍历所有子匹配器
            for (Matcher<T> matcher : matchers) {
                // 如果任一子匹配器匹配，立即返回true
                if (matcher.matching(target)) {
                    return true;
                }
            }
            // 所有子匹配器都不匹配，返回false
            return false;
        }

        /**
         * 向组合匹配器中添加一个新的子匹配器
         * <p>
         * 该方法用于动态扩展"或"组合的子匹配器列表。
         * 添加新的匹配器后，目标对象只需要满足原有匹配器或
         * 新添加的匹配器中的任意一个即可匹配成功。
         * </p>
         *
         * @param matcher 要添加的子匹配器，不能为null
         */
        @Override
        public void add(Matcher<T> matcher) {
            matchers.add(matcher);
        }
    }

}
