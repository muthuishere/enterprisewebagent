package com.enterprisewebagent.runtime.prompt;

import java.util.List;

public interface PromptAssembler {
    List<PromptSection> assemble(PromptContext context);
}
