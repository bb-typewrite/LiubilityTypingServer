package org.liubility.security.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.liubility.commons.dto.account.AccountDto;
import org.liubility.commons.exception.LBException;
import org.liubility.commons.http.response.normal.Result;
import org.liubility.security.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author JDragon
 * @Date 2021.02.11 上午 12:53
 * @Email 1061917196@qq.com
 * @Des:
 */

@RestController
@RequestMapping("/account")
@Api(tags = "用户账号")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/getLoginAccountByName")
    @ApiOperation("根据用户名获取登录用户")
    public AccountDto getLoginAccountByName(@RequestParam String username){
        return accountService.getLoginAccountByName(username);
    }

    @PostMapping("/login")
    @ApiOperation("登录")
    public Result<String> login(@RequestBody AccountDto accountDto) throws LBException {
        return Result.success(accountService.login(accountDto));
    }
}
