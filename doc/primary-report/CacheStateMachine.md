@startuml

state idle: 闲置状态
state check_dirty: 检查脏
state writeback
state replaceline: 替换行
state uncached

state uncached {
    state read
    state write
}

state writeback {
    state state0: 细分状态
    state state1: 细分状态
}

idle --> uncached: uncached
idle --> idle: cached and hitted
idle --> check_dirty: cached and miss

check_dirty --> writeback: is dirty
check_dirty --> replaceline: is not dirty

writeback --> replaceline

replaceline --> idle

@enduml