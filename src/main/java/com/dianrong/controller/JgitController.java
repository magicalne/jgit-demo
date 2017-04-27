package com.dianrong.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author: zehui.lv@dianrong on 4/27/17.
 */
@RestController("jgit")
public class JgitController {

    @RequestMapping(value = "/hello")
    public String hello() {
        return "hello";
    }
}
