' OmniDesk Universal PC Remote - Silent Background Runner
' Launches Python server with NO command prompt window so closing windows won't kill it.
Dim objShell, objFSO, strScriptDir, strPython, strServerPy

Set objShell = CreateObject("WScript.Shell")
Set objFSO = CreateObject("Scripting.FileSystemObject")

strScriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)
strServerPy = strScriptDir & "\server.py"

' Try to find pythonw.exe (windowless python) in virtualenv or PATH
If objFSO.FileExists(strScriptDir & "\.venv\Scripts\pythonw.exe") Then
    strPython = """" & strScriptDir & "\.venv\Scripts\pythonw.exe"""
ElseIf objFSO.FileExists(strScriptDir & "\venv\Scripts\pythonw.exe") Then
    strPython = """" & strScriptDir & "\venv\Scripts\pythonw.exe"""
Else
    strPython = "pythonw.exe"
End If

objShell.CurrentDirectory = strScriptDir
objShell.Run strPython & " """ & strServerPy & """", 0, False
Set objShell = Nothing
Set objFSO = Nothing
