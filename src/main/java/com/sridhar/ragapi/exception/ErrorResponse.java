package com.sridhar.ragapi.exception;


public record ErrorResponse(String type, String title, int status, String detail) {}