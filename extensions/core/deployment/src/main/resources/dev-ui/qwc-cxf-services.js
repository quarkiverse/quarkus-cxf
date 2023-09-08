import { LitElement, html, css} from 'lit'; 
import 'qui-card';
import '@vaadin/progress-bar';
import '@vaadin/grid'; 
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { JsonRpc } from 'jsonrpc';
import 'qui-ide-link';

/**
 * This component shows the List SOAP Web Services and clients
 */
export class QwcCxfServices extends LitElement { 
    jsonRpc = new JsonRpc(this);
    
    static styles = css` 
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
        _services: {state: true}, 
        _clients: {state: true}
    };
    
    constructor() { 
        super();
        this._services = null;
        this._clients = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getServices().then(jsonRpcResponse => { 
            this._services = jsonRpcResponse.result;
        });
        this.jsonRpc.getClients().then(jsonRpcResponse => { 
            this._clients = jsonRpcResponse.result;
        });
    }

    render() { 
        return html`${this._renderSoapServiceCard()}
                    ${this._renderSoapClientsCard()}`;
    }
    
    _renderSoapServiceCard(){
        return html`<qui-card title="SOAP Services">
            <div slot="content" class="content">
                ${this._renderSoapServices()}
            </div>
        </qui-card>`;
    }
    
    _renderSoapServices(){
        if(this._services){
            if(this._services.length>0) {
                return html`<vaadin-grid .items="${this._services}" theme="no-border" all-rows-visible>
                        <vaadin-grid-column auto-width
                            header="Class Name"
                            ${columnBodyRenderer(this._classNameRenderer, [])}
                            resizable>
                        </vaadin-grid-column>

                        <vaadin-grid-column auto-width
                            header="Path"
                            ${columnBodyRenderer(this._pathRenderer, [])}
                            resizable>
                        </vaadin-grid-column>
                    </vaadin-grid>`;
            }else {
                return html`<div class="nothing-found">No SOAP Services found</div>`;
            }
        }else{
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }
    
    _classNameRenderer(service){
        return html`<qui-ide-link title='Service class name'
                        class='text-source'
                        fileName='${service.className}'
                        lineNumber=0>${service.className}</qui-ide-link>`;
    }
    
    _pathRenderer(service) {
        return html`<code>${service.path}${service.relativePath}</code>`;
    }
    
    _renderSoapClientsCard(){
        return html`<qui-card title="SOAP Clients">
            <div slot="content">
                ${this._renderSoapClients()}
            </div>
        </qui-card>`;
    }
    
    _renderSoapClients(){
        if(this._clients){
            if(this._clients.length>0) {
                
                return html`<vaadin-grid .items="${this._clients}" theme="no-border" all-rows-visible>
                        <vaadin-grid-column auto-width
                            header="Service Endpoint Interface"
                            ${columnBodyRenderer(this._seiRenderer, [])}
                            resizable>
                        </vaadin-grid-column>

                        <vaadin-grid-column auto-width
                            header="Address"
                            ${columnBodyRenderer(this._endpointAddressRenderer, [])}
                            resizable>
                        </vaadin-grid-column>
                    </vaadin-grid>`;
            }else {
                return html`<div class="nothing-found">No SOAP Clients found</div>`;
            }
        }else{
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }
    
    _seiRenderer(client){
        return html`<qui-ide-link title='Generated class'
                        class='text-source'
                        fileName='${client.sei}'
                        lineNumber=0>${client.sei}</qui-ide-link>`;
    }
    
    _endpointAddressRenderer(client){
        if(client.wsdlUrl){
            return html`<code>${client.wsdlUrl}</code>`;
        }else if(client.endpointAddress){
            return html`<code>${client.endpointAddress}</code>`;
        }else {
            return html`<code>N/A</code>`;
        }
    }
    
}
customElements.define('qwc-cxf-services', QwcCxfServices);