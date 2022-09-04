package org.liubility.typing.server.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.liubility.commons.http.response.normal.Result;
import org.liubility.typing.server.code.convert.MockTypeConvert;
import org.liubility.typing.server.code.libs.TrieWordLib;
import org.liubility.typing.server.code.parse.SubscriptInstance;
import org.liubility.typing.server.code.parse.TrieWordParser;
import org.liubility.typing.server.compare.ArticleComparator;
import org.liubility.typing.server.compare.ComparisonItem;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: JDragon
 * @Data:2022/9/3 1:52
 * @Description:
 */
@RestController
@RequestMapping("/version/test")
@Api(tags = "测试")
public class TestController {

    private final TrieWordLib wordLib = new TrieWordLib("wordlib.txt", "23456789", 4, ";'");

    private final TrieWordLib symbol = new TrieWordLib("symbol.txt", "", 0, "");

    {
        wordLib.merge(symbol);
    }

    private final TrieWordParser trieWordParser = new TrieWordParser(wordLib, symbol, new MockTypeConvert("23456789", wordLib.getDefaultUpSymbol()));

    @PostMapping(value = "/typingTips")
    @ApiOperation("词提测试")
    public Result<SubscriptInstance[]> typingTips(@RequestBody CodeParam codeParam) {
        SubscriptInstance[] parse = trieWordParser.parse(codeParam.getCode());
        return Result.success(parse);
    }

    @PostMapping(value = "/codeLength")
    @ApiOperation("理论编码")
    public Result<String> codeLength(@RequestBody CodeParam codeParam) {
        SubscriptInstance[] parse = trieWordParser.parse(codeParam.getCode());
        String s = trieWordParser.printCode(parse);
        return Result.success(s);
    }

    @PostMapping(value = "/compare")
    @ApiOperation("看打听打提交成绩后的对比")
    public Result<List<ComparisonItem>> compare(@RequestBody CodeParam codeParam) {
        ArticleComparator articleComparator = new ArticleComparator();
        List<ComparisonItem> comparisonItemList = articleComparator.comparison(codeParam.getOrigin(), codeParam.getTyped(), codeParam.isIgnoreSymbols(), symbol);
        return Result.success(comparisonItemList);
    }

    @Data
    public static class CodeParam {

        private String code;

        private String origin;

        private String typed;

        private boolean ignoreSymbols;
    }
}
