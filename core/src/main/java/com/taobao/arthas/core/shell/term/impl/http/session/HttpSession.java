
package com.taobao.arthas.core.shell.term.impl.http.session;

import java.util.Enumeration;

/**
 * HTTP会话接口
 * <p>
 * 该接口定义了HTTP会话的基本行为，用于在HTTP连接之间维护状态信息。
 * 与Servlet规范中的HttpSession类似，但专门为Arthas的HTTP终端设计。
 * </p>
 *
 * @author hengyunabc 2021-03-03
 */
public interface HttpSession {

    /**
     * 获取会话创建时间
     * <p>
     * 返回该会话被创建的时间，以自1970年1月1日午夜GMT以来的毫秒数表示。
     * </p>
     *
     * @return 一个long值，指定该会话的创建时间，以自1970年1月1日GMT以来的毫秒数表示
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public long getCreationTime();

    /**
     * 获取会话唯一标识符
     * <p>
     * 返回包含分配给此会话的唯一标识符的字符串。
     * 标识符由Servlet容器分配，具体实现取决于容器。
     * </p>
     *
     * @return 指定分配给此会话的标识符的字符串
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public String getId();

    /**
     * 获取客户端最后访问时间
     * <p>
     * 返回客户端发送与此会话关联的请求的最后时间，以自1970年1月1日午夜GMT以来的毫秒数表示，
     * 时间标记为容器接收到请求的时间。
     * </p>
     * <p>
     * 应用程序执行的操作（如获取或设置与会话关联的值）不会影响访问时间。
     * </p>
     *
     * @return 一个long值，表示客户端发送与此会话关联的请求的最后时间，以自1970年1月1日GMT以来的毫秒数表示
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public long getLastAccessedTime();

    /**
     * 设置最大非活动间隔时间
     * <p>
     * 指定在Servlet容器使此会话失效之前，客户端请求之间的时间（以秒为单位）。
     * 零或负时间表示会话应该永不超时。
     * </p>
     *
     * @param interval 指定秒数的整数
     */
    public void setMaxInactiveInterval(int interval);

    /**
     * 获取最大非活动间隔时间
     * <p>
     * 返回Servlet容器在客户端访问之间保持此会话打开的最大时间间隔（以秒为单位）。
     * 超过此间隔后，Servlet容器将使会话失效。
     * 最大时间间隔可以通过<code>setMaxInactiveInterval</code>方法设置。
     * 零或负时间表示会话应该永不超时。
     * </p>
     *
     * @return 指定此会话在客户端请求之间保持打开的秒数的整数
     * @see #setMaxInactiveInterval
     */
    public int getMaxInactiveInterval();

    /**
     * 获取会话属性
     * <p>
     * 返回在此会话中使用指定名称绑定的对象，如果没有对象绑定在该名称下，则返回<code>null</code>。
     * </p>
     *
     * @param name 指定对象名称的字符串
     * @return 具有指定名称的对象
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public Object getAttribute(String name);

    /**
     * 获取所有属性名称
     * <p>
     * 返回包含绑定到此会话的所有对象名称的<code>String</code>对象的<code>Enumeration</code>。
     * </p>
     *
     * @return 指定绑定到此会话的所有对象名称的<code>String</code>对象的<code>Enumeration</code>
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public Enumeration<String> getAttributeNames();

    /**
     * 设置会话属性
     * <p>
     * 使用指定的名称将对象绑定到此会话。如果同名的对象已经绑定到会话，则替换该对象。
     * </p>
     * <p>
     * 在此方法执行后，如果新对象实现了<code>HttpSessionBindingListener</code>，
     * 容器将调用<code>HttpSessionBindingListener.valueBound</code>。
     * 然后容器通知Web应用程序中的任何<code>HttpSessionAttributeListener</code>。
     * </p>
     * <p>
     * 如果一个实现了<code>HttpSessionBindingListener</code>的对象已经绑定到此会话的此名称，
     * 将调用其<code>HttpSessionBindingListener.valueUnbound</code>方法。
     * </p>
     * <p>
     * 如果传入的值为null，则效果与调用<code>removeAttribute()</code>相同。
     * </p>
     *
     * @param name  对象绑定的名称；不能为null
     * @param value 要绑定的对象
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public void setAttribute(String name, Object value);

    /**
     * 移除会话属性
     * <p>
     * 从此会话中移除使用指定名称绑定的对象。
     * 如果会话没有使用指定名称绑定的对象，则此方法不执行任何操作。
     * </p>
     * <p>
     * 在此方法执行后，如果对象实现了<code>HttpSessionBindingListener</code>，
     * 容器将调用<code>HttpSessionBindingListener.valueUnbound</code>。
     * 然后容器通知Web应用程序中的任何<code>HttpSessionAttributeListener</code>。
     * </p>
     *
     * @param name 要从此会话中移除的对象的名称
     * @exception IllegalStateException 如果在已失效的会话上调用此方法
     */
    public void removeAttribute(String name);

    /**
     * 使会话失效
     * <p>
     * 使此会话失效，然后解除绑定到它的所有对象。
     * </p>
     *
     * @exception IllegalStateException 如果在已经失效的会话上调用此方法
     */
    public void invalidate();

    /**
     * 判断会话是否为新会话
     * <p>
     * 如果客户端还不知道此会话或客户端选择不加入此会话，则返回<code>true</code>。
     * 例如，如果服务器仅使用基于Cookie的会话，而客户端已禁用Cookie的使用，
     * 那么每次请求都会有一个新会话。
     * </p>
     *
     * @return 如果服务器已创建会话但客户端尚未加入，则返回<code>true</code>
     * @exception IllegalStateException 如果在已经失效的会话上调用此方法
     */
    public boolean isNew();
}
