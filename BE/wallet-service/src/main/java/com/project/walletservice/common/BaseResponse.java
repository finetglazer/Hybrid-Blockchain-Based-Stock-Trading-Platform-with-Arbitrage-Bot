package com.project.walletservice.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse<T> {
    private Integer status;
    private String error;
    private T data;
    // difference generic and object: https://stackoverflow.com/questions/2770321/difference-between-generic-type-and-object-type

    public BaseResponse(String error) {
        this.status = Const.STATUS_RESPONSE.ERROR;
        this.error = error;
    }
    public BaseResponse(T data) {
        this.status = Const.STATUS_RESPONSE.SUCCESS;
        this.data = data;
    }
}
