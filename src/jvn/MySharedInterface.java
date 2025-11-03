package jvn;

import annotations.JvnRead;
import annotations.JvnWrite;

public interface MySharedInterface {

	@JvnRead String getValue();
    @JvnWrite void setValue(String v);}
