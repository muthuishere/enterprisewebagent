# EXECUTION PIPELINE

## Intended Flow

1. client submits input from web or CLI
2. hosted runtime creates or resumes a runtime session
3. runtime-core assembles prompt and execution context
4. runtime-core resolves tool and capability profile
5. provider adapter invokes the selected model through Spring AI
6. runtime events are emitted during execution
7. tool and task updates are persisted
8. output is streamed back to the client

## Important Rule

The provider layer executes model calls.

The runtime layer decides:

- what prompt to assemble
- which role is running
- which tools are available
- what session and task state means
- how events are emitted and persisted

