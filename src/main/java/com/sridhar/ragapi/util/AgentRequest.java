package com.sridhar.ragapi.util;

import javax.validation.constraints.NotNull;

public record AgentRequest (@NotNull String userQuery){
}
