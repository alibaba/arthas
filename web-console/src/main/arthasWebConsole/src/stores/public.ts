import { defineStore } from 'pinia'
export const publicStore = defineStore('public', { // Public项目唯一id
    state: () => {
        return {
            userMsg: {},
        }
    },
    getters: {
        getUserMsg: (state) => {
            return state.userMsg
        },
    },
})
