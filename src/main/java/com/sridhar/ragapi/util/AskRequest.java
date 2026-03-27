package com.sridhar.ragapi.util;

import javax.validation.constraints.NotNull;

public record AskRequest (@NotNull String question){
}
