#!/bin/sh
set -eu

CONFIG=${RANGES_CONFIG:-/etc/device-simulator/conf/ranges.ini}

if [ ! -r "$CONFIG" ]; then
    echo "config file $CONFIG does not exist or is not readable" >&2
    exit 1
fi

ip_to_int() {
    old_ifs=$IFS
    IFS=.
    set -- $1
    IFS=$old_ifs
    echo $((($1 << 24) + ($2 << 16) + ($3 << 8) + $4))
}

int_to_ip() {
    echo "$((($1 >> 24) & 255)).$((($1 >> 16) & 255)).$((($1 >> 8) & 255)).$(($1 & 255))"
}

iface_for() {
    set -- $(ip route get "$1")
    while [ $# -gt 1 ]; do
        case $1 in
        via) return 1 ;;
        dev)
            echo "$2"
            return 0
            ;;
        esac
        shift
    done
    return 1
}

add_range() {
    if [ -z "$2" ] || [ -z "$3" ]; then
        echo "[$1] range_base and range_count are required" >&2
        exit 1
    fi

    if ! dev=$(iface_for "$2"); then
        echo "[$1] $2 is not on any network attached to this container" >&2
        exit 1
    fi

    start=$(ip_to_int "$2")
    i=0
    while [ "$i" -lt "$3" ]; do
        ip addr add "$(int_to_ip $((start + i)))/32" dev "$dev"
        i=$((i + 1))
    done

    echo "[$1] $2 - $(int_to_ip $((start + $3 - 1))) ($3 addresses) on $dev"
}

# --- read the INI file ---------------------------------------------------------
section="" base="" count=""

while IFS= read -r line || [ -n "$line" ]; do
    line=$(printf '%s' "$line" | tr -d ' \t\r') # drop all whitespace

    case $line in
    '' | '#'* | ';'*) # empty line or comment
        ;;
    '['*']') # new section: finish the previous one
        [ -n "$section" ] && add_range "$section" "$base" "$count"
        section=${line#[}
        section=${section%]}
        base="" count=""
        ;;
    range_base=*) base=${line#*=} ;;
    range_count=*) count=${line#*=} ;;
    *)
        echo "unexpected line in $CONFIG: $line" >&2
        exit 1
        ;;
    esac
done <"$CONFIG"

[ -n "$section" ] && add_range "$section" "$base" "$count"

trap 'exit 0' TERM INT
while :; do
    sleep 3600 &
    wait $!
done
