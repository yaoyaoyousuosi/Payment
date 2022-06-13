package org.rookie.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespBean<T> {
    private Integer code;
    private String message;
    private T data;

    public RespBean(Integer code, String message){
        this(code,message,null);
    }
}
