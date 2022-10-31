package org.liubility.typing.server.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.liubility.commons.util.FileUtils;
import org.liubility.typing.server.domain.dto.TypedWordsDTO;
import org.liubility.typing.server.domain.entity.TypingHistory;
import org.liubility.typing.server.enums.TypingWordsTypeEnum;
import org.liubility.typing.server.minio.service.MinioServiceImpl;
import org.liubility.typing.server.minio.service.OssFileInfoVO;
import org.liubility.typing.server.service.TypingHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: JDragon
 * @Data:2022/10/6 14:25
 * @Description: 打错词应该包含：词组错误，词组删除，词组拆分后错误，词组延伸后错误，编码删除。
 * 拆词应该包含：词组拆分正确
 * 漏词应该包含：词组延伸
 * 其他分类不确定：词组未错误删除
 * <p>
 * <p>
 * 词组状态：正确，错误
 * 词组删改状态：true,false
 * 词组对比状态：全等，拆分，延伸
 * 编码删改状态：true,false
 * 编码对比状态：全等，更简，更长
 */
@Service
public class TypingWordsDetailHandler {

    @Autowired
    private MinioServiceImpl minioService;

    @Autowired
    private TypingHistoryService typingHistoryService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveTypingWordsDetail(TypedWordsDTO typedWordsDTO, Long userId) {
        Long historyId = typedWordsDTO.getHistoryId();
        TypingHistory history = typingHistoryService.getById(historyId);
        if (history == null) {
            return;
        }
        List<TypedWordsDTO.Words> typeWords = typedWordsDTO.getTypeWords();
        int count = 0;
        int codeRight = 0;
        int wordRight = 0;
        int wordWrong = 0;
        int codeWrong = 0;
        int perfect = 0;
        Map<String, TypeStatusCount> map = new HashMap<>();
        for (TypedWordsDTO.Words typeWord : typeWords) {
            WordsDetailStatus wordsStatus = this.getWordsStatus(typeWord);
            count++;
            if (wordsStatus.codeRight()) {
                codeRight++;
            } else {
                codeWrong++;
            }
            if (wordsStatus.wordRight()) {
                wordRight++;
            } else {
                wordWrong++;
            }
            if (wordsStatus.perfect()) {
                perfect++;
            }
            String words = typeWord.mergeWords();
            TypeStatusCount typeStatusCount = map.computeIfAbsent(words, e -> new TypeStatusCount());
            typeStatusCount.incr(count, codeRight, wordRight, wordWrong, codeWrong, perfect);
        }
        minioService.upload(typedWordsDTO, "type-word",
                FileUtils.processingPaths(String.valueOf(userId), new SimpleDateFormat("yyyy-MM-dd").format(history.getTypeDate()), String.valueOf(historyId)));
        for (Map.Entry<String, TypeStatusCount> entry : map.entrySet()) {
            String key = entry.getKey();
            String redisKey = "type-word:" + userId;
            TypeStatusCount typeStatusCount = (TypeStatusCount) redisTemplate.opsForHash().get(redisKey, key);
            if (typeStatusCount == null) {
                typeStatusCount = new TypeStatusCount();
            }
            typeStatusCount.incr(entry.getValue());
            redisTemplate.opsForHash().put(redisKey, key, typeStatusCount);
        }
    }

    public WordsDetailStatus getWordsStatus(TypedWordsDTO.Words typeWord) {
        List<TypedWordsDTO.TypeChar> wordsChar = typeWord.getWordsChar();
        List<TypedWordsDTO.TypeChar> codesChar = typeWord.getCodesChar();
        boolean mistake = wordsChar.stream().anyMatch(TypedWordsDTO.TypeChar::getMistake);
        boolean result = wordsChar.stream().noneMatch(typeChar -> typeChar.getMistake() && typeChar.getDeleteTime() != null);
        String words = typeWord.mergeWords();
        String codes = typeWord.mergeCodes();

        Boolean wordRight = !mistake && result;
        long wordDelete = wordsChar.stream().filter(typeChar -> typeChar.getDeleteTime() != null).count();
        TypingWordsTypeEnum wordType = TypingWordsTypeEnum.compare(words, typeWord.getWordTips());
        long codeDelete = codesChar.stream().filter(typeChar -> typeChar.getDeleteTime() != null).count();
        TypingWordsTypeEnum codeType = TypingWordsTypeEnum.compare(codes, typeWord.getCodeTips());
        return new WordsDetailStatus(wordRight, wordDelete, wordType, codeDelete, codeType);
    }

    @Data
    @AllArgsConstructor
    public static class WordsDetailStatus {

        //词组状态：正确，错误
        private Boolean wordRight;

        //词组删改状态
        private long wordDelete;

        //词组对比状态
        private TypingWordsTypeEnum wordType;

        //编码删改状态
        private long codeDelete;

        //编码对比状态
        private TypingWordsTypeEnum codeType;

        public boolean perfect() {
            return wordRight() &&
                    codeRight() &&
                    wordType == TypingWordsTypeEnum.RIGHT &&
                    codeType == TypingWordsTypeEnum.RIGHT;
        }

        //回改
        public boolean wordRight() {
            return wordRight && wordDelete == 0;
        }

        //退格
        public boolean codeRight() {
            return codeDelete == 0;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStatusCount {

        private int count;

        private int codeRight;

        private int wordRight;

        private int codeWrong;

        private int wordWrong;

        private int perfect;

        public void incr(int count, int codeRight, int wordRight, int codeWrong, int wordWrong, int perfect) {
            this.count += count;
            this.codeRight += codeRight;
            this.wordRight += wordRight;
            this.codeWrong += codeWrong;
            this.wordWrong += wordWrong;
            this.perfect += perfect;
        }

        public void incr(TypeStatusCount typeStatusCount) {
            this.count += typeStatusCount.count;
            this.codeRight += typeStatusCount.codeRight;
            this.wordRight += typeStatusCount.wordRight;
            this.codeWrong += typeStatusCount.codeWrong;
            this.wordWrong += typeStatusCount.wordWrong;
            this.perfect += typeStatusCount.perfect;
        }
    }


}