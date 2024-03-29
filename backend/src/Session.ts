import { v4 as uuid } from 'uuid';

const delay = 500

export interface Student {
    phoneId: string,
    firstName: string,
    secondName: string
}

export class Session {
    //add verified field
    subjectName: string
    private idToName: { [id: string]: { firstName: string; secondName: string } } = {};

    private manuallyAdded: {firstName: string, secondName: string}[] = []

    private currentToken: string | null = null;
    private currentId: string | null = null;

    private _qrCode: string;

    public get qrCode() {
        return this._qrCode;
    }

    constructor(subjectName: string) {
        this.subjectName = subjectName;
        this._qrCode = uuid();

        setInterval(() => {
            this._qrCode = uuid();
        }, delay)
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
    //TODO: what if you get null or undefined as input? (CHECK TOP TODO IN index.ts)
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

    //called when student is added manually from to sync list of students between android and desktop
    manuallyAdd(student: {firstName: string, secondName: string}) {
        this.manuallyAdded.push(student)
    }

    manuallyDelete(student: {firstName: string, secondName: string}) {
        const index = this.manuallyAdded.indexOf(student)
        this.manuallyAdded.splice(index, 1)
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

    //get list of all students in this session as {id, first_name, second_name}[]
    getListOfStudents(): (Student | {firstName: string, secondName: string, phoneId: null})[] {
        const withId =  Object.keys(this.idToName).map(id => ({
            phoneId: id,
            firstName: this.idToName[id].firstName,
            secondName: this.idToName[id].secondName,
        }))
        const withoutId = this.manuallyAdded.map(s => ({...s, phoneId: null}))
        const result: (Student | {firstName: string, secondName: string, phoneId: null})[] = []
        for (let st of withId) {
            result.push(st)
        }
        for (let st of withoutId) {
            result.push(st)
        }
        return result
    }
}
