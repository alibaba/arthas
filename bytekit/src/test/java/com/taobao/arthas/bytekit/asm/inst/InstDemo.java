package com.taobao.arthas.bytekit.asm.inst;

import java.util.Date;

public class InstDemo {

	public int returnInt(int i) {
		return 9998;
	}

	public static int returnIntStatic(int i) {
		return 9998;
	}

	public static void main(String[] args) {
        Date date = new Date(1551168643);

        System.err.println(date.toLocaleString());
    }

}
