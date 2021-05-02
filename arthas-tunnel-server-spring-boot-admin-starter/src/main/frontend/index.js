/* global SBA */
import arthas from './arthas';

// tag::customization-ui-toplevel[]
SBA.use({
    install({viewRegistry}) {
        viewRegistry.addView({
            name: 'arthas',  //<1>
            path: '/arthas', //<2>
            component: arthas, //<3>
            label: 'Arthas', //<4>
            order: 1000, //<5>
        });
    }
});
// end::customization-ui-toplevel[]
