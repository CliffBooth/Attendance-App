interface Props {
    message: string,
    className?: string
}

const Alert = (props: Props) => {
    return <div className={`text-center p-3 border-4 border-red-600 bg-red-300 text-red-600 ${props.className ?? ""}`}>
        {props.message}
    </div>
}

export default Alert;