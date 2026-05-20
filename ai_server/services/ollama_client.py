import os

import requests

DEFAULT_BASE_URL = "http://localhost:11434"
DEFAULT_MODEL = "llama3.1:8b"


def is_ollama_enabled() -> bool:
    return os.getenv("OLLAMA_DOC_POSTPROCESS", "false").lower() == "true"


def summarize_document(title: str, text: str) -> str | None:
    prompt = (
        "You are helping process onboarding documents.\n"
        "Write a Korean admin preview summary in exactly 2 short sentences.\n"
        f"The document title is: {title}\n"
        "Focus on purpose, target audience, and key policy or procedure.\n"
        "Paraphrase the content instead of copying the opening lines.\n"
        "Do not copy menu text, navigation text, or headings without meaning.\n"
        "Do not use bullets.\n"
        "Return only the summary.\n\n"
        f"Document:\n{text[:4000]}"
    )
    return _generate_text(prompt)


def suggest_section_title(text: str) -> str | None:
    prompt = (
        "You are helping process onboarding documents.\n"
        "Create one short Korean section title for the following chunk.\n"
        "Limit it to 12 words or fewer.\n"
        "Return only the title.\n\n"
        f"Chunk:\n{text[:1500]}"
    )
    return _generate_text(prompt)


def generate_weather_encouragement(weather_desc: str, temp: float, hour: int) -> str | None:
    # 날씨, 기온, 시간을 조합한 맞춤형 응원 메시지 생성 (이름은 프론트엔드에서 처리하므로 제외)
    prompt = (
        "You are a warm and professional HR assistant named 'COREWORK AI'. "
        "Write a one-sentence Korean encouragement message for an employee based on the context. "
        "Do NOT include the user's name or any greetings like 'Hello' or 'Hi'. "
        f"Context - Weather: {weather_desc}, Temperature: {temp}C, Time: {hour}:00. "
        "Do not use emojis unless they are very subtle. Keep it concise.\n"
        "Return only the message text without any extra notes."
    )
    return _generate_text(prompt)


def chat(prompt: str) -> str | None:
    """
    일반적인 대화형 프롬프트 처리
    """
    return _generate_text(prompt)


def _generate_text(prompt: str) -> str | None:
    if not is_ollama_enabled():
        return None

    base_url = os.getenv("OLLAMA_BASE_URL", DEFAULT_BASE_URL).rstrip("/")
    model = os.getenv("OLLAMA_MODEL", DEFAULT_MODEL)

    response = requests.post(
        f"{base_url}/api/generate",
        json={
            "model": model,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": 0.2,
            },
        },
        timeout=60,
    )
    response.raise_for_status()

    payload = response.json()
    text = (payload.get("response") or "").strip()
    return text or None
