package com.project.userservice.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class BaseResponse<T> {
    private Integer status;
    private String msg;
    private T data;
}
