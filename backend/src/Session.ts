import { v4 as uuid } from 'uuid';

export class Session {
    //add verified field
    idToName: { [id: string]: { firstName: string; secondName: string } } = {};

    private currentToken: string | null = null;
    private currentId: string | null = null;

    private _qrCode: string;

    public get qrCode() {
        return this._qrCode;
    }

    constructor() {
        this._qrCode = uuid();
    }

    /**
     * Puts assosiates name with id in the dictionary and also creates unique token for the name
     *
     * @param str = "${firstName}:${seconNmae}:${id}"
     * @throws Error if string has wrong structure
     */
    //TODO: check if elements empty!
    //TODO: maybe throw custom exception if the same id?
    //better to throw exception if there are not 3 elements and return false if id already exists
    //TODO: what if you get null or andefined as input? (CHECK TOP TODO IN index.ts)
    putName(str: string) {
        let params = str.split(':');
        if (params.length !== 3) throw Error('Wrong string structure');

        let [firstName, secondName, id] = params;
        this.currentToken = uuid();
        this.currentId = id;
        this.idToName[id] = { firstName, secondName };
    }

    //called from /bluetooth endpoint to not set currentId and not generate toekn
    saveName(str: string) {
        let params = str.split(':');
        if (params.length !== 3) throw Error('Wrong string structure');

        let [firstName, secondName, id] = params;
        this.idToName[id] = { firstName, secondName }
    }

    /**
     * @param str = "${firstName}:${seconNmae}:${id}"
     * @throws Error if string has wrong structure
     * @returns true - if name has been added, false - if id already exists
     */
    contains(str: string): boolean {
        let params = str.split(':');
        if (params.length !== 3) throw Error('Wrong string structure');

        let [firstName, secondName, id] = params;
        if (this.idToName[id]) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param str = "${firstName}:${seconNmae}:${id}"
     * @throws Error if string has wrong structure
     * @returns true - if name current session is with this phone, false - otherwise
     */
    isCurrent(str: string): boolean {
        let params = str.split(':');
        if (params.length !== 3) throw Error('Wrong string structure');

        let [firstName, secondName, id] = params;
        return id === this.currentId;
    }

    /**
     * @returns object containing name of current student
     */
    getCurrentName(): { firstName: string; secondName: string } {
        if (this.currentId === null) throw Error('current id is null!');
        return this.idToName[this.currentId];
    }

    //TODO: null safety
    getToken(): string {
        return this.currentToken!!;
    }
}
