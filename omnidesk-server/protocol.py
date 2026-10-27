import json
from typing import Any, Dict, Optional

class ProtocolError(Exception):
    pass

class RemoteMessage:
    @staticmethod
    def parse(raw_data: str) -> Dict[str, Any]:
        try:
            data = json.loads(raw_data)
            if not isinstance(data, dict):
                raise ProtocolError("Message payload must be a JSON object")
            if "type" not in data:
                raise ProtocolError("Message missing required 'type' field")
            return data
        except json.JSONDecodeError as e:
            raise ProtocolError(f"Malformed JSON: {e}")

    @staticmethod
    def success(req_type: str, request_id: Optional[str] = None, **kwargs) -> str:
        payload = {
            "type": f"{req_type}_result",
            "success": True,
            **kwargs
        }
        if request_id:
            payload["request_id"] = request_id
        return json.dumps(payload)

    @staticmethod
    def error(req_type: str, message: str, request_id: Optional[str] = None, code: int = 400) -> str:
        payload = {
            "type": f"{req_type}_result",
            "success": False,
            "error": message,
            "code": code
        }
        if request_id:
            payload["request_id"] = request_id
        return json.dumps(payload)

    @staticmethod
    def notification(notif_type: str, **kwargs) -> str:
        payload = {
            "type": notif_type,
            **kwargs
        }
        return json.dumps(payload)
