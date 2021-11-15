export MOB_TIMER_URL=http://localhost:8080/
export MOB_TIMER_ROOM=test
MOB_TIMER_USER=test1 mob t 1
sleep 1
MOB_TIMER_USER=test2 mob t 1
sleep 1
MOB_TIMER_USER=test1 mob break 1
sleep 1
MOB_TIMER_USER=test3 mob t 1
sleep 1
MOB_TIMER_USER=test3 mob t 1
sleep 1
MOB_TIMER_USER=test1 mob break 1
sleep 1
MOB_TIMER_USER=test2 mob t 1
sleep 1
MOB_TIMER_USER=test2 mob t 0
sleep 1
MOB_TIMER_USER=test2 mob t 1