# AI Test Suggestion Tool

A CLI tool that analyzes Kotlin source files and suggests meaningful test cases using a local LLM via [Ollama](https://ollama.com/).

## Overview

The tool receives a Kotlin file or directory as a command-line argument, sends the source code to a locally-running LLM, and returns structured test suggestions — each with a name, description, expected behavior, and priority. It uses Jetbrains' Koog library to talk to the LLMs

### Why a one-shot CLI instead of a REPL?

A REPL-based approach was considered but discarded for two reasons:

1. **No tool-use support in the target models.** The Ollama models explored (`qwen2.5-coder:14b` and `deepseek-r1:14b`) do not support tool calling, which would be needed for the LLM to read files and navigate directories on its own. Without tools the REPL would be limited to human-pasted code snippets.
2. **Focused UX.** The program is designed to do one thing — generate test suggestions for a given path. A one-shot CLI constrains the input to exactly what the tool needs (a file or directory), rather than opening a free-form prompt where users can type anything the program is not optimized to handle.

### Thinker model experiment

A thinker model ([`deepseek-r1:14b`](https://ollama.com/library/deepseek-r1)) was also tested. While it produced chain-of-thought reasoning, the speed was significantly slower and the quality of the final suggestions was not noticeably better than `qwen2.5-coder:14b`, so it was not adopted.

### Hardware tested on

- **GPU:** NVIDIA GeForce RTX 4070 (12 GB VRAM)
- **RAM:** 64 GB

## Requirements

- [Ollama](https://ollama.com/) running locally on the default port (`localhost:11434`)
- The required model [`qwen2.5-coder:14b`](https://ollama.com/library/qwen2.5-coder:14b) pulled and running beforehand

## Usage

```bash
# Analyze a single Kotlin file
./gradlew run --args="path/to/File.kt"

# Analyze all Kotlin files in a directory (test files are excluded automatically)
./gradlew run --args="path/to/src/"

# Verbose mode — shows per-file progress
./gradlew run --args="--verbose path/to/src/"
```

## Sample output

See [SAMPLE_OUTPUT.txt](SAMPLE_OUTPUT.txt) for a real output of the tool's execution on a kotlin test project
