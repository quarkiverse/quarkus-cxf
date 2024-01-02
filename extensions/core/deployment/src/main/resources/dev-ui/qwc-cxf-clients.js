import { LitElement, html, css} from 'lit';
import 'qui-card';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { JsonRpc } from 'jsonrpc';
import 'qui-ide-link';

/**
 * This component shows the list of clients
 */
export class QwcCxfClients extends LitElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        .cxf-table {
          height: 100%;
          padding-bottom: 10px;
        }

        code {
          font-size: 85%;
        }

        .annotation {
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
        _clients: {state: true}
    };

    constructor() {
        super();
        this._clients = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getClients().then(jsonRpcResponse => {
            this._clients = jsonRpcResponse.result;
        });
    }

    render() {
        if (this._clients) {
            if (this._clients.length > 0) {
                return this._renderClientList();
            } else {
                return html`<div class="nothing-found">No clients found</div>`;
            }
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }

    _renderClientList(){
        return html`<vaadin-grid .items="${this._clients}" class="cxf-table" theme="no-border" all-rows-visible>
                <vaadin-grid-column auto-width
                    header="Service Endpoint Interface (SEI)"
                    ${columnBodyRenderer(this._classNameRenderer, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column auto-width
                    header="Address"
                    ${columnBodyRenderer(this._addressRenderer, [])}
                    resizable>
                </vaadin-grid-column>

                <vaadin-grid-column auto-width
                    header="WSDL"
                    ${columnBodyRenderer(this._wsdlRenderer, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _classNameRenderer(client){
        return html`<vaadin-vertical-layout>
                        <code class="annotation">@CXFClient("${client.configKey}")</code>
                        <qui-ide-link title='Service Endpoint Interface (SEI)'
                            fileName='${client.sei}'
                            lineNumber=0><code>${client.sei}</code></qui-ide-link>
                    </vaadin-vertical-layout>`;
    }

    _addressRenderer(client) {
        return html`<vaadin-vertical-layout>
                        <code class="annotation">&nbsp;</code>
                        <code><a href="${client.address}" target="_blank">${client.address}</a></code>
                    </vaadin-vertical-layout>`;
    }

    _wsdlRenderer(client) {
        return html`<vaadin-vertical-layout>
                        <code class="annotation">&nbsp;</code>
                        <code><a href="${client.wsdl}" target="_blank">${client.wsdl}</a></code>
                    </vaadin-vertical-layout>`;
    }

}
customElements.define('qwc-cxf-clients', QwcCxfClients);