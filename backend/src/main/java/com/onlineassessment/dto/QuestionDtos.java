package com.onlineassessment.dto;
import com.onlineassessment.entity.*; import jakarta.validation.constraints.*; import java.util.*;
public class QuestionDtos {
 public record OptionRequest(@NotBlank String optionText,boolean correct,Integer displayOrder){}
 public record Request(@NotBlank String questionText,@NotNull QuestionType type,@NotNull Difficulty difficulty,@NotNull @Positive Double marks,@NotNull @PositiveOrZero Double negativeMarks,String explanation,@NotNull List<OptionRequest> options,Integer displayOrder,String starterCode,List<Integer> allowedLanguageIds,String testCasesJson){}
 public record PublicOption(Long id,String optionText,Integer displayOrder){}
 public record PublicQuestion(Long id,String questionText,QuestionType type,Difficulty difficulty,Double marks,Double negativeMarks,Integer displayOrder,List<PublicOption> options,String starterCode,List<Integer> allowedLanguageIds){}
 public record AdminQuestion(Long id,String questionText,QuestionType type,Difficulty difficulty,Double marks,Double negativeMarks,String explanation,Integer displayOrder,List<AdminOption> options,String starterCode,List<Integer> allowedLanguageIds,String testCasesJson){}
 public record AdminOption(Long id,String optionText,boolean correct,Integer displayOrder){}
}
