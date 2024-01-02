import { LitElement, html, css} from 'lit';
import 'qui-card';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { JsonRpc } from 'jsonrpc';
import 'qui-ide-link';

/**
 * This component shows the list of Service endpoints
 */
export class QwcCxfServices extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        .cxf-table {
          height: 100%;
          padding-bottom: 10px;
        }

        code {
          font-size: 85%;
        }

        .service-sei {
          color: var(--lumo-contrast-50pct);
        }

        :host {
            display: flex;
            flex-direction:column;
            gap: 20px;
            padding-left: 10px;
            padding-right: 10px;
        }
        .nothing-found {
            padding: 5px;
        }`;

    static properties = {
        _services: {state: true}
    };

    constructor() {
        super();
        this._services = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getServices().then(jsonRpcResponse => {
            this._services = jsonRpcResponse.result;
        });
    }

    render() {
        if (this._services) {
            if (this._services.length > 0) {
                return this._renderServiceList();
            }else {
                return html`<div class="nothing-found">No service endpoints found</div>`;
            }
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }

    _renderServiceList(){
        return html`<vaadin-grid .items="${this._services}" class="cxf-table" theme="no-border" all-rows-visible>
                <vaadin-grid-column auto-width
                    header="Implementor"
                    ${columnBodyRenderer(this._classNameRenderer, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column auto-width
                    header="WSDL"
                    ${columnBodyRenderer(this._wsdlRenderer, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _classNameRenderer(service){
                        /* service.sei always the same as service.className
                        <qui-ide-link title='Implementor'
                            fileName='${service.sei}'
                            lineNumber=0><code class="service-sei">${service.sei}</code></qui-ide-link>
                        */
        return html`<vaadin-vertical-layout>
                        <qui-ide-link title='Implementor'
                            fileName='${service.implementor}'
                            lineNumber=0><code>${service.implementor}</code></qui-ide-link>
                    </vaadin-vertical-layout>`;
    }

    _wsdlRenderer(service) {
        return html`<code><a href="${service.path}?wsdl" target="_blank">${service.path}?wsdl</a></code>`;
    }

}
customElements.define('qwc-cxf-services', QwcCxfServices);
