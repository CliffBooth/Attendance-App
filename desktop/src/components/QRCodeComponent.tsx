import { Slider } from '@mui/material';
import { FunctionComponent, useContext, useState } from 'react';
import QRCode from 'react-qr-code';

interface Props {
    value: string
    size?: number
}

const QRCodeComponent: FunctionComponent<Props> = (props) => {

    const [size, setSize] = useState<number>(props.size!!);

    return (
        <div className="relative flex flex-col items-center">
            <div className="flex justify-center items-center m-auo h-[450px]">
                <div className="border-2 border-black">
                    <QRCode value={props.value} size={size} />
                </div>
            </div>
            <div className="w-[500px] p-5">
                <Slider
                    defaultValue={props.size!!}
                    onChange={(e, value) => setSize(value as number)}
                    min={props.size!! - (props.size!! / 2)}
                    max={props.size!! + (props.size!! / 2)}
                />
            </div>
        </div>
    );
};

QRCodeComponent.defaultProps = {
    size: 500
  };

export default QRCodeComponent;
