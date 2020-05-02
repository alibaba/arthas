
package com.taobao.arthas.bytekit.asm.location;

public enum LocationType {
    /**
     * user define.
     */
    USER_DEFINE,
    
    /**
     * method enter.
     *
     */
    ENTER,
    /**
     * line number.
     *
     */
    LINE,
    /**
     * field read operation.
     *
     */
    READ,
    /**
     * field read operation.
     */
    READ_COMPLETED,
    /**
     * field write operation.
     *
     */
    WRITE,
    /**
     * field write operation.
     *
     */
    WRITE_COMPLETED,
    /**
     * method invoke operation
     *
     */
    INVOKE,
    /**
     * method invoke operation
     *
     */
    INVOKE_COMPLETED,
    
    /**
     * method invoke exception
     */
    INVOKE_EXCEPTION_EXIT,
    /**
     * synchronize operation
     *
     */
    SYNC_ENTER,
    /**
     * synchronize operation
     *
     */
    SYNC_ENTER_COMPLETED,
    
    /**
     * synchronize operation
     *
     */
    SYNC_EXIT,
    /**
     * synchronize operation
     *
     */
    SYNC_EXIT_COMPLETED,

    /**
     * throw
     */
    THROW,

    /**
     * return
     */
    EXIT,

    /**
     * add try/catch
     */
    EXCEPTION_EXIT;

}
