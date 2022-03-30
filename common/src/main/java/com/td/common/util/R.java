/**
 * Copyright 2022 lotey. All rights reserved.
 * Use of this source code is governed by a MIT-style
 * license that can be found in the LICENSE file.
 */
package com.td.common.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @param <T>
 */
@Builder
@ToString
@Accessors(chain = true)
@AllArgsConstructor
public class R<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private int code = 200;

	@Getter
	@Setter
	private String msg = "OK";


	@Getter
	@Setter
	private T data;

	public R() {
		super();
	}

	public R(T data) {
		super();
		this.data = data;
	}

	public R(T data, String msg) {
		super();
		this.data = data;
		this.msg = msg;
	}

	public R(Throwable e) {
		super();
		this.msg = e.getMessage();
		this.code = 500;
	}

	public static R ok() {
		return new R();
	}

	public static R error() {
		return error(500, "未知异常，请联系管理员");
	}

	public static R error(int code, String msg) {
		R r = new R();
		r.setCode(code);
		r.setMsg(msg);
		return r;
	}
}
