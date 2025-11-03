#!/usr/bin/env bash
set -euo pipefail

# dossiers
SRC_DIR=src
OUT_DIR=out
LOG_DIR=logs

mkdir -p "$OUT_DIR" "$LOG_DIR"

echo "1) Compilation..."
javac -d "$OUT_DIR" $(find "$SRC_DIR" -name "*.java") || { echo "Compilation failed"; exit 1; }

echo "2) Lancer le coordinateur (JvnCoordImpl)..."
# lance en arrière-plan et sauvegarde PID
java -cp "$OUT_DIR" jvn.JvnCoordImpl > "$LOG_DIR/coord.log" 2>&1 &
PID_COORD=$!
echo "  PID_COORD=$PID_COORD (logs: $LOG_DIR/coord.log)"

# give coordinator a bit of time to bind RMI registry
sleep 1

echo "3) Lancer JVM1 (création & register)..."
java -cp "$OUT_DIR" test.TestCreateRegisterCache > "$LOG_DIR/jvm1.log" 2>&1 &
PID_JVM1=$!
echo "  PID_JVM1=$PID_JVM1 (logs: $LOG_DIR/jvm1.log)"

sleep 1

echo "4) Lancer JVM2 (lookup & read)..."
java -cp "$OUT_DIR" test.TestLookupReadCache > "$LOG_DIR/jvm2.log" 2>&1 &
PID_JVM2=$!
echo "  PID_JVM2=$PID_JVM2 (logs: $LOG_DIR/jvm2.log)"

sleep 1

echo "5) Lancer JVM3 (write demo)..."
java -cp "$OUT_DIR" test.TestWriteThenReadCache > "$LOG_DIR/jvm3.log" 2>&1 &
PID_JVM3=$!
echo "  PID_JVM3=$PID_JVM3 (logs: $LOG_DIR/jvm3.log)"

echo
echo "Tous les processus sont lancés."
echo "Tails logs with:"
echo "  tail -f $LOG_DIR/coord.log $LOG_DIR/jvm1.log $LOG_DIR/jvm2.log $LOG_DIR/jvm3.log"
echo
echo "Appuie sur Ctrl+C pour tout arrêter proprement."

# Trap Ctrl+C to kill background processes
cleanup() {
  echo
  echo "Arrêt des processus..."
  kill $PID_JVM3 $PID_JVM2 $PID_JVM1 $PID_COORD 2>/dev/null || true
  wait 2>/dev/null || true
  echo "Terminé."
  exit 0
}
trap cleanup INT

# Keep script running so you can tail logs in another terminal, or let it wait
while true; do sleep 1; done
