import ClassCompnent from "./ClassComponent"

interface Props {

}

const DateComponent = () => {

    return (
        <div className="container mx-auto border-2 shadow-md p-2">
            <div className="px-3">
                Date
            </div>
            <div className="h-2 bg-gray-300 my-3"></div>
            <div className="flex space-x-3 px-3">
                <ClassCompnent />
                <ClassCompnent />
            </div>
        </div>
    )
}

export default DateComponent