#!/bin/sh
#
# example-play2-scala_app service
#
# chkconfig:   35 99 99
# description: example-play2-scala_app service

# Source function library.
. /etc/rc.d/init.d/functions

prog="example-play2-scala_app"
exec="java -jar -Dhttp.port=8080 /usr/lib/example-play2-scala_app/example-play2-scala_app.jar"
output_logfile=/var/log/example-play2-scala_app/output.log
pidfile="/var/run/example-play2-scala_app/example-play2-scala_app.pid"

[ -e /etc/sysconfig/$prog ] && . /etc/sysconfig/$prog

lockfile=/var/lock/subsys/$prog

start() {
    echo -n $"Starting $prog: "
    daemon --pidfile=${pidfile} $exec
    retval=$?
    echo
    [ $retval -eq 0 ] && touch $lockfile
    return $retval
}

stop() {
    echo -n $"Stopping $prog: "
    killproc -p $pidfile
    retval=$?
    echo
    [ $retval -eq 0 ] && rm -f $lockfile
    echo "Application stopped." >> $output_logfile
    return $retval
}

force_reload() {
    restart
}

reload() {
    restart
}

restart() {
    stop
    start
}

rh_status() {
    # run checks to determine if the service is running or use generic status
    status -p $pidfile $prog
}

rh_status_q() {
    rh_status >/dev/null 2>&1
}

case "$1" in
    start)
        rh_status_q && exit 0
        $1
        ;;
    stop)
        rh_status_q || exit 0
        $1
        ;;
    restart)
        $1
        ;;
    reload)
        rh_status_q || exit 7
        $1
        ;;
    force-reload)
        force_reload
        ;;
    status)
        rh_status
        ;;
    condrestart|try-restart)
        rh_status_q || exit 0
        restart
        ;;
    *)
        echo $"Usage: $0 {status|start|restart|stop|condrestart|try-restart|reload|force-reload}"
        exit 2
esac
exit $?
