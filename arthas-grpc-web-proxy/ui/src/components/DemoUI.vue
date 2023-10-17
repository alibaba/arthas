<template>
  <Menu mode="horizontal" :theme="theme1"  @on-select="turnUrl" active-name="$route.name">
    <MenuItem name="vmtool">
      <Icon type="ios-construct" />
      Vmtool
    </MenuItem>
    <MenuItem name="watch">
      <Icon type="ios-paper" />
      Watch
    </MenuItem>
    <MenuItem name="sysprop">
      <Icon type="ios-construct" />
      Sysprop
    </MenuItem>

    <MenuItem name="pwd">
      <Icon type="ios-construct" />
      Pwd
    </MenuItem>
  </Menu>
</template>

<script>

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'DemoUI',
  data(){
    return {
      theme1: 'light'
    };
  },


  created() {
    this.$router.push('/vmtool');
  },

  methods:{
    turnUrl(name){
      this.$router.push(name);
    }
  }
}

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

</style>
