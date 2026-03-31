package com.sridhar.ragapi.util;

import jakarta.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record AskRequest (@NotBlank @Size(max=500) String question){
}
