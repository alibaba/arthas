import { defineStore } from 'pinia'
export const publicStore = defineStore('public', { // Public项目唯一id
    state: () => ({
            userMsg: {},
            isErr:false,
            ErrMessage:''
    }),
    getters: {
        getUserMsg: (state) => {
            return state.userMsg
        },
    },
})
